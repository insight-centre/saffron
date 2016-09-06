package org.insightcentre.nlp.saffron.documentindex.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

/**
 * A utility for making Lucene Documents.
 * 
 * @author Georgeta Bordea
 * 
 */
public class LuceneDocument {

	public static final String UID_NAME = "uid";
	public static final String CONTENTS_NAME = "contents";

	public static Document makeDocument(String id, String text) {
		Document doc = new Document();
		doc.add(new StringField(UID_NAME, id, Field.Store.YES));
		doc.add(new TextField(CONTENTS_NAME, text, Field.Store.YES));
		//TODO: Position offsets in Lucene 4?
		return doc;
	}
}
