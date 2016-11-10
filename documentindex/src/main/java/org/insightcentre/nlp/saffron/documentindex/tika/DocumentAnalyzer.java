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
public class DocumentAnalyzer {

    public static Document analyze(Document d) throws IOException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        InputStream stream = TikaInputStream.get(d.file.toPath());
        try {
            parser.parse(stream, handler, metadata);
        } catch (SAXException | TikaException ex) {
            throw new IOException(ex);
        }
        d.setContents(handler.toString());
        if(metadata.get(Metadata.CONTENT_TYPE) != null)
            d.mimeType = metadata.get(Metadata.CONTENT_TYPE);
        if(metadata.getValues(Office.AUTHOR) != null && d.authors.isEmpty()) {
            d.authors = new ArrayList<>();
            for(String names : metadata.getValues(Office.AUTHOR))
                for(String name : names.split(","))
                    d.authors.add(new Author(name.trim()));
        }
        if(metadata.get(TikaCoreProperties.TITLE) != null
            && !metadata.get(TikaCoreProperties.TITLE).equals("")) 
            d.name = metadata.get(TikaCoreProperties.TITLE);
        return d;
    }
}
