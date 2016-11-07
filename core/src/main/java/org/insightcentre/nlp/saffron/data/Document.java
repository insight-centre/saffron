package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * A single document in a corpus
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class Document {
    public final File file;
    public final String id;
    public final String name;
    public final String mimeType;
    public final List<Author> authors;

    @JsonCreator
    public Document(@JsonProperty(value="file", required=true) File file, 
                    @JsonProperty(value="id", required=true) String id,
                    @JsonProperty("name") String name,
                    @JsonProperty("mimeType") String mimeType, 
                    @JsonProperty("authors") List<Author> authors) {
        this.file = file;
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.authors = authors;
    }

    public String getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

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