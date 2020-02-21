package org.insightcentre.nlp.saffron.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An object describing a Partonomy of part-whole relationships comprising of a collection of Taxonomy components
 * 
 * @author Andy Donald
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Partonomy {

    /** A list of Taxonomy objects */
    private List<Taxonomy> components;


    protected Partonomy() {
        components = new ArrayList<Taxonomy>();
    }

    @JsonCreator
    @JsonIgnoreProperties(ignoreUnknown = true)
    public Partonomy(@JsonProperty("components") List<Taxonomy> children) {
        this.components = children;
    }
    
    /**
     * Build a Partonomy object from a JSON file
     * @param file The json file to read from
     * @return a Partonomy object
     * 
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static Partonomy fromJsonFile(File file) throws JsonParseException, JsonMappingException, IOException{
    	ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    	return objectMapper.readValue(file, Partonomy.class);
    }


    /**
     * Get the list of taxonomy components
     * @return A list of taxonomy objects
     */
    public List<Taxonomy> getComponents() {
        return components;
    }

    /**
     * Verify constraints for partonomy
     * TODO: This is probably where the constraint implementation can be placed
     * @return true if there are no loops
     */
    public boolean verifyPartonomy() {
        throw new NotImplementedException("Partonomy.verifyPartonomy not implemented");
    }
    
    /**
     * Retrieve all relation pairs with a given {@link Status}
     * 
     * @param status - the status of the pairs to be retrieved
     * @return a {@link Set} with all relations with that status
     * 
     * @author Bianca Pereira
     */
    public Set<TypedLink> getRelationsByStatus(Status status) {
    	Set<TypedLink> relations = new HashSet<TypedLink>();  
    	
    	for(Taxonomy component: this.getComponents()) {
    		relations.addAll(component.getRelationsByStatus(status));
    	}
    	
    	return relations;
    }

    /**
     * Build a Partonomy object from a string
     * @param json file to read from
     * @return a partonomy object
     *
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static Partonomy fromJsonString(String json) throws JsonParseException, JsonMappingException, IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        json = json.substring(1, json.length()-1);
        return objectMapper.readValue(json, Partonomy.class);
    }



    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.components);
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
        final Partonomy other = (Partonomy) obj;

        if(this.components.size() != other.components.size())
        	return false;
        for(Taxonomy component: this.components) {
        	if (!other.components.contains(component))
        		return false;
        }

        return true;
    }

    @Override
    public String toString() {
        String returnString = "";
        for (Taxonomy t : components) {
            returnString.concat(t.toString());
        }
        return returnString;
    }
    
    public static class Builder{
    	
    	private Partonomy partonomy;
    	
    	public Builder() {
            partonomy = new Partonomy();
    	}
    	
    	public Builder(Partonomy partonomy) {
    		this.partonomy = partonomy;
    	}

    	public Builder addComponent(Taxonomy component) {
            partonomy.components.add(component);
    		
    		return this;
    	}
    	
    	public Partonomy build() {
    		return partonomy;
    	}
	}
}
