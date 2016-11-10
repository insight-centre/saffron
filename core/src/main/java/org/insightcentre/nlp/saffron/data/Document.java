package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A single document in a corpus
 * 
 * @author John McCrae <john@mccr.ae>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"contents"})
public class Document {
    public final File file;
    public final String id;
    public String name;
    @JsonProperty("mime_type")
    public String mimeType;
    public List<Author> authors;
    private String contents;
    public Map<String, String> metadata;

    @JsonCreator
    public Document(@JsonProperty(value="file", required=true) File file, 
                    @JsonProperty(value="id", required=true) String id,
                    @JsonProperty("name") String name,
                    @JsonProperty("mime_type") String mimeType, 
                    @JsonProperty("authors") List<Author> authors,
                    @JsonProperty("metadata") Map<String, String> metadata) {
        this.file = file;
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.authors = authors == null ? new ArrayList<Author>() : authors;
        this.metadata = metadata == null ? new HashMap<String, String>() : metadata;
    }

    /**
     * Set the contents of this file once they have been loaded (i.e., from the source file
     * 
     * @param contents The text contents of this document
     */
    public void setContents(String contents) {
        this.contents = contents;
    }

    /**
     * Have the contents of the document been loaded
     * @return True if the document contents are loaded
     */
    public boolean hasContents() {
        return contents != null;
    }
            
    
    /**
     * Get the text contents of the document
     * @return The contents
     * @throw IllegalArgumentException If the document has not been loaded
     */
    public String getContents() {
        if(contents == null) {
            throw new IllegalArgumentException("Document has not been loaded yet. Please use DocumentSearcher to index corpus");
        }
        return contents;
    }
    
    public String getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    @JsonProperty("mime_type")
    public String getMimeType() {
        return mimeType;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.id);
        hash = 67 * hash + Objects.hashCode(this.name);
        hash = 67 * hash + Objects.hashCode(this.authors);
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
        final Document other = (Document) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.authors, other.authors)) {
            return false;
        }
        return true;
    }
    
}