package org.insightcentre.nlp.saffron.documentindex.lucene;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.insightcentre.nlp.saffron.data.Author;

/**
 * A utility for making Lucene Documents.
 * 
 * @author Georgeta Bordea
 * 
 */
public class LuceneDocument {

	public static final String UID_NAME = "uid";
	public static final String CONTENTS_NAME = "contents";
	public static final String SOURCE_FILE = "contents";
	public static final String AUTHORS_NAME = "authors";
	public static final String FULL_NAME = "full_name";
	public static final String MIME_TYPE = "mime";

	public static Document makeDocument(String id, String text, List<Author> authors, String fullName, File original, String mimeType) {
		Document doc = new Document();
		doc.add(new StringField(UID_NAME, id == null ? "" : id , Field.Store.YES));
		doc.add(new TextField(CONTENTS_NAME, text == null ? "" : text, Field.Store.YES));
		doc.add(new TextField(SOURCE_FILE, original == null ? "" : original.getAbsolutePath(), Field.Store.YES));
		doc.add(new TextField(FULL_NAME, fullName == null ? "" : fullName, Field.Store.YES));
		doc.add(new TextField(AUTHORS_NAME, mkAuthors(authors), Field.Store.YES));
		doc.add(new TextField(MIME_TYPE, mimeType == null ? "" : mimeType, Field.Store.YES));
		return doc;
	}

    private static String mkAuthors(List<Author> authors) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(authors);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<Author> unmkAuthors(String field) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(field, mapper.getTypeFactory().constructCollectionType(List.class, Author.class));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


}
