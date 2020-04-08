package org.insightcentre.nlp.saffron.documentindex.lucene;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.SaffronPath;

/**
 * A utility for making Lucene Documents.
 * 
 * @author Georgeta Bordea
 * 
 */
public class LuceneDocument {

	public static final String UID_NAME = "uid";
	public static final String URL = "url";
	public static final String CONTENTS_NAME = "contents";
	public static final String SOURCE_FILE = "source_file";
	public static final String AUTHORS_NAME = "authors";
	public static final String FULL_NAME = "full_name";
	public static final String MIME_TYPE = "mime";
	public static final String METADATA = "metadata";
        public static final String DATE_NAME = "date";

	public static Document makeDocument(String id, String text, URL url, List<Author> authors, String fullName, File original, String mimeType, Map<String, String> metadata, String date) {
		Document doc = new Document();
		doc.add(new StringField(UID_NAME, id == null ? "" : id , Field.Store.YES));
		doc.add(new TextField(CONTENTS_NAME, text == null ? "" : text, Field.Store.YES));
		doc.add(new TextField(URL, url == null ? "" : url.toString(), Field.Store.YES));
		doc.add(new TextField(SOURCE_FILE, original == null ? "" : original.getAbsolutePath(), Field.Store.YES));
		doc.add(new TextField(FULL_NAME, fullName == null ? "" : fullName, Field.Store.YES));
		doc.add(new TextField(AUTHORS_NAME, mkAuthors(authors), Field.Store.YES));
		doc.add(new TextField(MIME_TYPE, mimeType == null ? "" : mimeType, Field.Store.YES));
		doc.add(new TextField(METADATA, metadata == null ? "{}" : mkMetadata(metadata), Field.Store.YES));
		doc.add(new TextField(DATE_NAME, date == null ? "" : date, Field.Store.YES));
		return doc;
	}

    private static ObjectMapper mapper = new ObjectMapper();
    private static String mkAuthors(List<Author> authors) {
        try {
            return mapper.writeValueAsString(authors);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<Author> unmkAuthors(String field) {
        try {
            return mapper.readValue(field, mapper.getTypeFactory().constructCollectionType(List.class, Author.class));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String mkMetadata(Map<String, String> authors) {
        try {
            return mapper.writeValueAsString(authors);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Map<String, String> unmkMetadata(String field) {
        try {
            return mapper.readValue(field, mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }





}
