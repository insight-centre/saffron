package org.insightcentre.nlp.saffron.benchmarks;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import static org.insightcentre.nlp.saffron.benchmarks.TaxonomyExtractionBenchmark.readTExEval;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.wikiclean.WikiClean;

/**
 * Create a benchmark from a gold standard test set
 *
 * @author John McCrae
 */
public class BenchmarkFromWikipedia {

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("w", "A wikipedia dump").withRequiredArg().ofType(File.class);
                    accepts("g", "The gold taxonomy").withRequiredArg().ofType(File.class);
                    accepts("o", "The output folder").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            final File goldFile = (File) os.valueOf("g");
            if (goldFile == null || !goldFile.exists()) {
                badOptions(p, "Gold taxonomy not specified or does not exist");
                return;
            }

            final File wikiFile = (File) os.valueOf("w");
            if (wikiFile == null || !wikiFile.exists()) {
                badOptions(p, "Wikipedia file not specified");
                return;
            }

            final File outputFile = (File) os.valueOf("o");
            if (outputFile.exists() && !outputFile.isDirectory() || !outputFile.exists() && !outputFile.mkdirs()) {
                badOptions(p, "The output directory could not be created as a directory");
                return;
            }

            final BufferedReader reader;
            if (wikiFile.getName().endsWith(".gz")) {
                reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(wikiFile))));
            } else if (wikiFile.getName().endsWith(".bz2")) {
                reader = new BufferedReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(wikiFile))));
            } else {
                reader = new BufferedReader(new FileReader(wikiFile));
            }

            final Set<String> titles;
            if (goldFile.getName().endsWith(".json")) {
                Taxonomy t = mapper.readValue(goldFile, Taxonomy.class);
                titles = extractTitles(t);
            } else {
                titles = extractTitles2(readTExEval(goldFile));
            }

            Random random = new Random();
            WikiDumpCorpus wikiCorpus = new WikiDumpCorpus(reader, titles);
            List<Document> corpus = new ArrayList<>();
            while (wikiCorpus.hasNext()) {
                Document d = wikiCorpus.next();
                System.err.println(d.name);
                File f = new File(outputFile, d.name + ".txt");
                while (f.exists()) {
                    f = new File(outputFile, d.name + random.nextInt(10000) + ".txt");
                }
                try (PrintWriter out = new PrintWriter(f)) {
                    out.println(d.contents());
                }
                corpus.add(new Document(SaffronPath.fromFile(f), d.id, d.url, d.name, d.mimeType, d.authors, d.metadata, null, d.date));
            }
            mapper.writeValue(new File(outputFile, "corpus.json"),
                    new SimpleCorpus(corpus));

        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }

    }
    
    private static final class SimpleCorpus implements Corpus {
        public final List<Document> documents;

        public SimpleCorpus(List<Document> documents) {
            this.documents = documents;
        }

        @Override
        public Iterable<Document> getDocuments() {
            return documents;
        }

        @Override
        public int size() {
            return documents.size();
        }
        
        
    }

    private static final Pattern TITLE = Pattern.compile(".*<title>(.*)</title>.*");
    private static final Pattern TEXT = Pattern.compile(".*<text .*>.*");
    private static final Pattern END_TEXT = Pattern.compile(".*</text>.*");

    private static Set<String> extractTitles(Taxonomy t) {
        Set<String> titles = new HashSet<>();
        _extractTitles(t, titles);
        return titles;
    }

    private static Set<String> extractTitles2(Set<TaxonomyExtractionBenchmark.StringPair> readTExEval) {
        Set<String> title = new HashSet<>();
        for (TaxonomyExtractionBenchmark.StringPair sp : readTExEval) {
            title.add(sp._1.toLowerCase());
            title.add(sp._2.toLowerCase());
        }
        return title;
    }

    private static void _extractTitles(Taxonomy t, Set<String> titles) {
        titles.add(t.root);
        for (Taxonomy t2 : t.children) {
            _extractTitles(t2, titles);
        }
    }

    private static class WikiDumpCorpus implements Iterator<Document> {

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
        private final Set<String> docTitles;

        public WikiDumpCorpus(BufferedReader reader, Set<String> docTitles) {
            this.reader = reader;
            this.docTitles = docTitles;
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
                        inArticle = false;
                        try {
                            if (docTitles.contains(title.toLowerCase())) {
                                String contents = cleaner.clean(sb.toString());
                                return new Document(null, title, null, title, "text/plain", Collections.EMPTY_LIST, Collections.EMPTY_MAP, contents, null);
                            } 
                        } catch (Exception x) {
                            x.printStackTrace();
                        }
                    } else {
                        sb.append(line).append("\n");
                    }
                }
                return null;

            } catch (IOException x) {
                x.printStackTrace();
                return null;
            }
        }
    }
}
