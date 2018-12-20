package org.insightcentre.nlp.saffron.documentindex.tika;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Office;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.xml.sax.SAXException;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class DocumentAnalyzer {

    public static String removeLigatures(String s) {
        return s.replaceAll("\ufb00", "ff").
                replaceAll("\ufb03", "ffi").
                replaceAll("\ufb04", "ffl").
                replaceAll("\ufb01", "fi").
                replaceAll("\ufb02", "fl");

    }

    public static Document analyze(File f, String id) throws IOException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        final InputStream stream;
            stream = TikaInputStream.get(f.toPath());
        try {
            parser.parse(stream, handler, metadata);
        } catch (SAXException | TikaException ex) {
            throw new IOException(ex);
        }

        String mimeType = null;
        if (metadata.get(Metadata.CONTENT_TYPE) != null) {
            mimeType = removeLigatures(metadata.get(Metadata.CONTENT_TYPE));
        }
        List<Author> authors = new ArrayList<>();
        if (metadata.getValues(Office.AUTHOR) != null) {
            for (String names : metadata.getValues(Office.AUTHOR)) {
                for (String name : names.split(",")) {
                    authors.add(new Author(removeLigatures(name.trim())));
                }
            }
        }
        String name = null;
        if (metadata.get(TikaCoreProperties.TITLE) != null
                && !metadata.get(TikaCoreProperties.TITLE).equals("")) {
            name = removeLigatures(metadata.get(TikaCoreProperties.TITLE));
        }
        String contents = removeLigatures(handler.toString());
        return new Document(SaffronPath.fromFile(f), id, null, name, mimeType, authors, new HashMap<>(), contents);
    }
    
    public static Document analyze(InputStream stream, String id) throws IOException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        try {
            parser.parse(stream, handler, metadata);
        } catch (SAXException | TikaException ex) {
            throw new IOException(ex);
        }

        String mimeType = null;
        if (metadata.get(Metadata.CONTENT_TYPE) != null) {
            mimeType = removeLigatures(metadata.get(Metadata.CONTENT_TYPE));
        }
        List<Author> authors = new ArrayList<>();
        if (metadata.getValues(Office.AUTHOR) != null) {
            for (String names : metadata.getValues(Office.AUTHOR)) {
                for (String name : names.split(",")) {
                    authors.add(new Author(removeLigatures(name.trim())));
                }
            }
        }
        String name = id;
        if (metadata.get(TikaCoreProperties.TITLE) != null
                && !metadata.get(TikaCoreProperties.TITLE).equals("")) {
            name = removeLigatures(metadata.get(TikaCoreProperties.TITLE));
        }
        String contents = removeLigatures(handler.toString());
        return new Document(null, id, null, name, mimeType, authors, new HashMap<>(), contents);
        
    }

}
