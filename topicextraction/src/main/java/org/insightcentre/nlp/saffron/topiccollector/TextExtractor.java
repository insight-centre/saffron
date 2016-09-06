package org.insightcentre.nlp.saffron.topiccollector;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.insightcentre.nlp.saffron.topicextraction.topicextractor.exceptions.UnsupportedFileTypeException;

public class TextExtractor {

    public static String extractText(File f) throws IOException, DocumentConversionException {
        InputStream is = new FileInputStream(f);
        String mimeType = new Tika().detect(is);
        is.close();

        is = new FileInputStream(f);
        String text = extractText(is, mimeType);
        is.close();

        return text;
    }

    public static String extractText(BufferedInputStream is) throws IOException, DocumentConversionException {
        String detectedMimeType = new Tika().detect(is);
        is.reset(); //Go back to start of stream for re-reading
        //logger.info("Detected MIME type as " + detectedMimeType);
        return extractText(is, detectedMimeType);
    }

    public static String extractText(InputStream is, String mimeType) throws IOException, DocumentConversionException {
        String text;
        if (mimeType.equals("text/plain")) {
            text = IOUtils.toString(is, "UTF-8");
        } else if (mimeType.equals("application/pdf")) {
            text = getContentPDFBox(is);
        } else {
            throw new UnsupportedFileTypeException("Unsupported file, unrecognised MIME type " + mimeType);
        }
        return preprocessText(text);
    }

    public static String getContentPDFBox(InputStream is) throws IOException, DocumentConversionException {
        try {
            ContentHandler textHandler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            PDFParser parser = new PDFParser();
            ParseContext pc = new ParseContext();

            parser.parse(is, textHandler, metadata, pc);

            return textHandler.toString();
        } catch (SAXException | TikaException e) {
            throw new DocumentConversionException(e.getMessage(), e);
        } finally {
            is.close();
        }
    }

    public static String preprocessText(String content) {
        /*
         * remove word separation characters used at the end of line, except
         * where they occur elsewhere in the text.
         * 
         * e.g. "sys-\ntem" should be converted to "system"
         * e.g. "peer-\nto-peer" should still be "peer-to-peer",
         *      we keep the "-" if the term "peer-to-peer" appears elsewhere in the document
         */

        Pattern whitespace = Pattern.compile(
            //Match word ending with '-' that goes on to the next line
            "(\\w+)-\\s*\\n"
            + //optional spaces or a second '-' at beginning of next line
            "\\s*-?"
            + //word including dashes on the second line
            "([\\w-]+)"
        );
        Matcher matcher = whitespace.matcher(content);
        StringBuffer result = new StringBuffer();
        HashSet<String> blah = new HashSet<String>();
        while (matcher.find()) {
            String mergedWord = matcher.group(1) + "-" + matcher.group(2);
            if (content.contains(mergedWord)) {
                matcher.appendReplacement(result, mergedWord);
            } else {
                blah.add(mergedWord);
                matcher.appendReplacement(result, "$1$2");
            }
        }
        matcher.appendTail(result);
        content = result.toString();

        return content;
    }
}
