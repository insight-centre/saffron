package org.insightcentre.nlp.saffron.documentindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.SaffronPath;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Main {
   private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: "  + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
 
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {{
                accepts("c", "The file containing the corpus metadata OR a folder all files of which will be automatically processed").withRequiredArg().ofType(File.class);
                accepts("t", "The file to write the processed corpus to").withRequiredArg().ofType(File.class);
                accepts("d", "The place to write processed documents to").withRequiredArg().ofType(File.class);
            }};
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
 
            final File corpusFile = (File)os.valueOf("c");
            if(corpusFile == null || !corpusFile.exists()) {
                badOptions(p, "Corpus does not exist");
                return;
            }
            final File indexFile = (File)os.valueOf("t");
            if(indexFile == null) {
                badOptions(p, "A target to write the processed files to must be specified");
                return;
            }
            File targetDir = (File)os.valueOf("d");
            if(targetDir == null) {
                targetDir = indexFile.getParentFile();
            }
            if(!targetDir.exists())
                if(!targetDir.mkdirs()) {
                    System.err.println("Could not make dir " + targetDir);
                    return;
                }

            final Corpus corpus = CorpusTools.readFile(corpusFile);

            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(indexFile, new ProcessedCorpus(corpus, targetDir));
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }

    private static final long MAX_DOC_IN_JSON_SIZE = 10000;

    private static class ProcessedCorpus implements Corpus {
        private final Corpus corpus;
        private final File targetFolder;

        public ProcessedCorpus(Corpus corpus, File targetFolder) {
            this.corpus = corpus;
            this.targetFolder = targetFolder;
        }


        @Override
        public Iterable<Document> getDocuments() {
            return new Iterable<Document>() {
                @Override
                public Iterator<Document> iterator() {
                    final Iterator<Document> i = corpus.getDocuments().iterator();
                    return new Iterator<Document>() {
                        @Override
                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        @Override
                        public Document next() {
                            Document d = i.next();
                            if((d.file == null && d.getContents() != null && d.getContents().length() > MAX_DOC_IN_JSON_SIZE)
                                    || (d.file != null && !d.file.getPath().endsWith(".txt"))
                                    || (d.file == null && d.getContents() == null)) {

                                File target = new File(targetFolder, pathEscape(d.id.endsWith(".txt") ? d.id.substring(0,d.id.length()-4) : d.id) + ".txt");
                                try(FileWriter out = new FileWriter(target)) {
                                    out.write(d.contents());
                                } catch(IOException x) {
                                    throw new RuntimeException(x);
                                }
                                return new Document(SaffronPath.fromFile(target), d.id, null, d.name, "text/plain", d.authors, d.metadata, null, d.date);
                            } else {
                                return d;
                            }
                        }
                    };
                }
            };
        }

        @Override
        public int size() {
            return corpus.size();
        }


    }

    private static String pathEscape(String path) {
        return path.replace(File.separator, "__");
    }
}
