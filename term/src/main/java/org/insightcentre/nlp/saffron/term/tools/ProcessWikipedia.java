package org.insightcentre.nlp.saffron.term.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.insightcentre.nlp.saffron.term.FrequencyStats;
import org.insightcentre.nlp.saffron.term.TermExtraction;
import org.wikiclean.WikiClean;

/**
 *
 * @author John McCrae
 */
public class ProcessWikipedia {

    private static final Pattern TITLE = Pattern.compile(".*<title>(.*)</title>.*");
    private static final Pattern TEXT = Pattern.compile(".*<text .*>.*");
    private static final Pattern END_TEXT = Pattern.compile(".*</text>.*");

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Please specify the path to the Wikipedia dump, config and output");
            System.exit(-1);
        }
        final BufferedReader reader;
        if (args[0].endsWith(".gz")) {
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(args[0]))));
        } else if (args[0].endsWith(".bz2")) {
            reader = new BufferedReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(args[0]))));
        } else {
            reader = new BufferedReader(new FileReader(args[0]));
        }
        ObjectMapper mapper = new ObjectMapper();
        TermExtractionConfiguration config = mapper.readValue(new File(args[1]), Configuration.class).termExtraction;

        TermExtraction extractor = new TermExtraction(config);

        FrequencyStats stats = extractor.extractStats(new DocumentSearcher() {
            @Override
            public Iterable<Document> allDocuments() throws SearchException {
                return new Iterable<Document>() {
                    @Override
                    public Iterator<Document> iterator() {
                        return new WikiDumpAsSearcher(reader);
                    }
                };
            }

            @Override
            public void close() throws IOException {
            }
        });

        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(args[2]), stats);
    }

    private static class WikiDumpAsSearcher implements Iterator<Document> {

        String line = "";
        String title = "";
        //PrintWriter out = null;
        //File tmpFile = null;
        boolean inArticle = false;
        List<Document> docs = new ArrayList<>();
        final WikiClean cleaner = new WikiClean.Builder().build();
        StringBuilder sb = new StringBuilder();
        private final BufferedReader reader;
        private Document next;

        public WikiDumpAsSearcher(BufferedReader reader) {
            this.reader = reader;
            this.next = doNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Document next() {
            Document d = next;
            next = doNext();
            return d;
        }

        private Document doNext() {
            try {
                while ((line = reader.readLine()) != null) {
                    if (!inArticle) {
                        Matcher m = TITLE.matcher(line);
                        if (m.matches()) {
                            title = m.group(1);
                        }
                        if (TEXT.matcher(line).matches() && !END_TEXT.matcher(line).matches()) {
                            //tmpFile = File.createTempFile("wiki-" + title.replaceAll("\\W", ""), ".txt");
                            //tmpFile.deleteOnExit();
                            //out = new PrintWriter(tmpFile);
                            inArticle = true;
                            sb = new StringBuilder(line);
                        }
                    } else if (END_TEXT.matcher(line).matches()) {
                        sb.append(line);
                        //out.println(cleaner.clean(sb.toString()));
                        //out.close();
                        //out = null;
                        inArticle = false;
                        return new Document(null, title, null, title, "text/plain", Collections.EMPTY_LIST, Collections.EMPTY_MAP, sb.toString());
                    } else {
                        sb.append(line).append("\n");
                    }
                }
                return null;

            } catch (IOException x) {
                throw new RuntimeException(x);
            }
        }
    }
}
