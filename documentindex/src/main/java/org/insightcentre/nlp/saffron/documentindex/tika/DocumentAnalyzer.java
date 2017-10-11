package org.insightcentre.nlp.saffron.documentindex.tika;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Office;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class DocumentAnalyzer implements Document.Loader {

    public static String removeLigatures(String s) {
        return s.replaceAll("\ufb00", "ff").
                replaceAll("\ufb03", "ffi").
                replaceAll("\ufb04", "ffl").
                replaceAll("\ufb01", "fi").
                replaceAll("\ufb02", "fl");

    }

    public Document analyze(Document d) throws IOException {
        if (d.file != null || d.url != null) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            final InputStream stream;
            if(d.file != null) {
                stream = TikaInputStream.get(d.file.toPath());
            } else {
                stream = TikaInputStream.get(d.url);
            }
            try {
                parser.parse(stream, handler, metadata);
            } catch (SAXException | TikaException ex) {
                throw new IOException(ex);
            }

            if (metadata.get(Metadata.CONTENT_TYPE) != null) {
                d.mimeType = removeLigatures(metadata.get(Metadata.CONTENT_TYPE));
            }
            if (metadata.getValues(Office.AUTHOR) != null && d.authors.isEmpty()) {
                d.authors = new ArrayList<>();
                for (String names : metadata.getValues(Office.AUTHOR)) {
                    for (String name : names.split(",")) {
                        d.authors.add(new Author(removeLigatures(name.trim())));
                    }
                }
            }
            if (metadata.get(TikaCoreProperties.TITLE) != null
                    && !metadata.get(TikaCoreProperties.TITLE).equals("")) {
                d.name = removeLigatures(metadata.get(TikaCoreProperties.TITLE));
            }
            return d.withLoader(this);
        } else {
            return d;
        }
    }

    @Override
    public String getContents(Document d) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler(-1);
            InputStream stream = TikaInputStream.get(d.file.toPath());
            try {
                parser.parse(stream, handler, metadata);
            } catch (SAXException | TikaException ex) {
                throw new IOException(ex);
            }
            return removeLigatures(handler.toString());
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public String getContentsSerializable(Document d) {
        return null;
    }
    
    
}
