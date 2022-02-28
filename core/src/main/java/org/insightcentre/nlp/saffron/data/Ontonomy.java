package org.insightcentre.nlp.saffron.data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.Taxonomy.Builder;


/**
 * <h1> Ontonomy Class for atLocation, usedFor, hasPrerequisite Relations!</h1>
 * The Ontonomy class implements data structure to store and retrieve
 * three types of relations (<i>atLocation, usedFor, hasPrerequisite</i>)
 *
 * @author Jamal Nasir
 * @version 1.0
 * @since 01-12-2020
 */
public class Ontonomy implements SaffronGraph<TypedLink> {


    public Set<TypedLink> relations;

    /**
     * Creates an empty ontonomy
     */
    public Ontonomy() {

        this.relations = new HashSet<TypedLink>();

    }

    /**
     * Creates an Ontonomy as a deep copy of another Ontonomy plus a link to be added
     *
     * @param toCopy  - The Ontonomy to be copied
     * @param newLink - the new link to be added
     */
    public Ontonomy(Ontonomy toCopy, TypedLink newLink) {

        Ontonomy deepCopy = (Ontonomy) toCopy.clone();
        this.relations = deepCopy.getRelationsByStatus(Status.rejected);
        this.relations.add(new TypedLink(newLink));

    }

    @Override
    public Object clone() {
        Ontonomy ontonomy;
        try {
            ontonomy = (Ontonomy) super.clone();
        } catch (CloneNotSupportedException e) {
            ontonomy = new Ontonomy();
        }
        for (TypedLink relation: this.relations) {
            ontonomy.relations.add(new TypedLink(relation));
        }


        return ontonomy;
    }

    // Retrieves all the relations within the ontonomy.

    /**
     * This method retrieves all the relations within the ontonomy.
     *
     * @param status: Currently not using in the method
     * @return Relations in a Set
     */
    @Override
    public Set<TypedLink> getRelationsByStatus(Status status) {


        return this.relations;
    }

    /**
     * Override method for equals
     *
     * @param o
     * @return
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ontonomy ontonomy = (Ontonomy) o;
        return relations.equals(ontonomy.relations);
    }

    /**
     * Override method for hashCode
     *
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(relations);
    }

    public static class Builder{

    	private Ontonomy ontonomy;

    	public Builder() {
    		ontonomy = new Ontonomy();
    	}

    	public Builder(Ontonomy ontonomy) {
    		this.ontonomy = ontonomy;
    	}

    	public Builder addRelation(TypedLink relation) {
    		ontonomy.relations.add(relation);
    		return this;
    	}

    	public Ontonomy build() {
    		return ontonomy;
    	}
	}
}
