package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A single document in a corpus
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Document {

    public final SaffronPath file;
    public final String id;
    public final URL url;
    public String name;
    @JsonProperty("mime_type")
    public String mimeType;
    public List<Author> authors;
    private Loader contents;
    public Map<String, String> metadata;
    @JsonSerialize(using = DateSerializer.class)
    @JsonDeserialize(using = DateDeserializer.class)
    public LocalDateTime date;

    @JsonCreator
    public Document(@JsonProperty(value = "file") SaffronPath file,
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "url") URL url,
            @JsonProperty("name") String name,
            @JsonProperty("mime_type") String mimeType,
            @JsonProperty("authors") List<Author> authors,
            @JsonProperty("metadata") Map<String, String> metadata,
            @JsonProperty("contents") String contents,
            @JsonProperty("date") LocalDateTime date) {
        if (file == null && contents == null && url == null) {
            System.err.println("id=" + id);
        }
        this.file = file;
        this.url = url;
        this.id = id;
        this.name = name;
        this.mimeType = mimeType == null && contents != null ? "text/plain" : mimeType;
        this.authors = authors == null ? new ArrayList<Author>() : authors;
        this.metadata = metadata == null ? new HashMap<String, String>() : metadata;
        this.date = date;
        if (contents != null) {
            this.contents = new InMemory(contents);
        } else if (file != null) {
            this.contents = new OnDisk();
        } else if (url != null) {
            this.contents = new Remote();
        } else {
            throw new IllegalArgumentException("Please give either document contents or link to file");
        }
    }

    /**
     * Set the contents of this file once they have been loaded (i.e., from the
     * source file)
     *
     * @param contents A (possibly lazy) loader for the contents
     */
    public Document withLoader(Loader contents) {
        // Skip this in the case that the contents are in memory
        if (this.contents == null || !(this.contents instanceof InMemory)) {
            this.contents = contents;
        }
        return this;
    }

    /**
     * Get the *raw* text contents of the document. This method will not load
     * files to read contents, unlike contents()
     *
     * @return The contents
     * @throws IllegalArgumentException If the document has not been loaded
     */
    public String getContents() {
        if (contents != null) {
            return contents.getContentsSerializable(this);
        } else {
            return null;
        }
    }

    /**
     * Retrieve the contents of the document. Note this method cause documents
     * to be read from disc
     *
     * @return The contents of the document
     */
    public String contents() {
        if (contents == null) {
            throw new IllegalArgumentException("Cannot retrieve contents, deserialization method not set");
        } else {
            return contents.getContents(this);
        }
    }

    public String getId() {
        return id;
    }

    public SaffronPath getFile() {
        return file;
    }

    public Map<String, String> getMetadata() {
        return metadata;
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

    /**
     * Enables on-demand loading of document contents
     */
    public static interface Loader {

        /**
         * The contents of the document
         *
         * @return The contents, possibly loading from a disk or internal source
         */
        String getContents(Document d);

        /**
         * The contents or null if they are serialized elsewhere
         *
         * @return The contenst, or null if they are serialized elsewhere
         */
        String getContentsSerializable(Document d);
    }

    /**
     * Document is a string in memory
     */
    public static class InMemory implements Loader {

        private final String contents;

        public InMemory(String contents) {
            this.contents = contents;
        }

        @Override
        public String getContents(Document d) {
            return contents;
        }

        @Override
        public String getContentsSerializable(Document d) {
            return contents;
        }
    }

    public static class OnDisk implements Loader {

        @Override
        public String getContents(Document d) {
            if (d.file != null) {
                try (BufferedReader reader = new BufferedReader(new FileReader(d.file.toFile()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                } catch (IOException x) {
                    throw new RuntimeException(x);
                }
            } else {
                throw new UnsupportedOperationException("File not available");
            }
        }

        @Override
        public String getContentsSerializable(Document d) {
            return null;
        }

    }

    public static class Remote implements Loader {

        @Override
        public String getContents(Document d) {
            if (d.url != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(d.url.openStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                } catch (IOException x) {
                    throw new RuntimeException(x);
                }
            } else {
                throw new UnsupportedOperationException("URL not available");
            }
        }

        @Override
        public String getContentsSerializable(Document d) {
            return null;
        }

    }

    /**
     * Return a copy of this document focusing only on a single term
     *
     * @param term
     * @param contextSize
     * @return
     */
    public Document reduceContext(String term, int contextSize) {
        ArrayList<String> words = new ArrayList<>(Arrays.asList(contents().toLowerCase().split("\\b")));
        int index = words.indexOf(term);
        if (index >= 0) {
            List<String> context = words.subList(Math.max(0, index - contextSize), Math.min(words.size(), index + contextSize));
            StringBuilder sb = new StringBuilder();
            for (String c : context) {
                sb.append(c);
            }
            return new Document(this.file, this.id, this.url, this.name, this.mimeType, this.authors, this.metadata, sb.toString(), this.date);
        } else {
            return new Document(this.file, this.id, this.url, this.name, this.mimeType, this.authors, this.metadata, "", this.date);
        }
    }

    @Override
    public String toString() {
        if (contents != null && contents instanceof InMemory) {
            return String.format("Document(%s,InMemory)", id);
        } else if (contents != null && contents instanceof OnDisk) {
            return String.format("Document(%s,%s)", id, file.toFile().getAbsolutePath());
        } else if (contents != null && contents instanceof Remote) {
            return String.format("Document(%s,%s)", id, url);
        } else if (contents != null) {
            return String.format("Document(%s,%s)", id, contents.toString());
        } else {
            return String.format("Document(%s,NoContent)", id);
        }
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
            "yyyy[-MM[-dd[[ ]['T']HH:mm[:ss][XXX]]]]");

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(
            "yyyy[-MM[-dd]]");

    /**
     * Parse a date
     *
     * @param date The date as an ISO String
     * @return The date object
     */
    public static LocalDateTime parseDate(String s) {
        if(s == null || s.equals(""))
            return null;
        if (s.matches("\\d{4}")) {
            return LocalDateTime.of(Integer.parseInt(s), Month.JANUARY, 1, 0, 0);
        } else if (s.matches("\\d{4}-\\d{1,2}")) {
            return LocalDateTime.of(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(5, 7)), 1, 0, 0);
        }
        try {
            return LocalDateTime.parse(s, formatter);
        } catch (Exception x) {
            return LocalDate.parse(s, dateFormatter).atStartOfDay();
        }
    }

    @JsonIgnore
    public String getDateAsString() {
        if (date == null) {
            return null;
        }
        return formatter.format(date);
    }

    public static class DateDeserializer extends StdDeserializer<LocalDateTime> {

        public DateDeserializer() {
            super(LocalDateTime.class);
        }

        @Override
        public LocalDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String s = jp.getText();
            return parseDate(s);
        }

    }

    public static class DateSerializer extends StdSerializer<LocalDateTime> {

        public DateSerializer() {
            super(LocalDateTime.class);
        }

        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "yyyy[-MM[-dd[[ ]['T']HH:mm[:ss][XXX]]]]");

        @Override
        public void serialize(LocalDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException {
            jg.writeString(formatter.format(t));
        }

    }
}
