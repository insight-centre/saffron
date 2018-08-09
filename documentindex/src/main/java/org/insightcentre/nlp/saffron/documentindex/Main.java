package org.insightcentre.nlp.saffron.documentindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;
import org.insightcentre.nlp.saffron.data.SaffronPath;

/**
 *
 * @author John McCrae <john@mccr.ae>
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
                accepts("c", "The file containing the corpus metadata OR a folder all files of which will be automatically indexed").withRequiredArg().ofType(File.class);
                accepts("i", "The file to write the index to").withRequiredArg().ofType(File.class);
                accepts("o", "Where to write the output corpus metadata file to").withRequiredArg().ofType(File.class);
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
                badOptions(p, "Configuration does not exist");
                return;
            }
            final File indexFile = (File)os.valueOf("i");
            final File outputFile = (File)os.valueOf("o");
            
            ObjectMapper mapper = new ObjectMapper();
            // Read configuration
            final Corpus corpus;
            
            if(corpusFile.isDirectory()) {
                if(indexFile == null) {
                    badOptions(p, "Corpus file is a folder but index file is null");
                }
                if(outputFile == null) {
                    badOptions(p, "Corpus file is a folder but output file not specified");
                }
                corpus = CorpusTools.fromFolder(corpusFile);
                mapper.writeValue(outputFile, toIndexed(corpus, SaffronPath.fromFile(indexFile)));
            } else if(corpusFile.getName().endsWith(".zip")) {
                if(indexFile == null) {
                    badOptions(p, "Corpus file is a ZIP but index file is null");
                }
                if(outputFile == null) {
                    badOptions(p, "Corpus file is a ZIP but output file not specified");
                }
                corpus = CorpusTools.fromZIP(corpusFile, new File(outputFile.getParent(), "docs"));
                mapper.writeValue(outputFile, toIndexed(corpus, SaffronPath.fromFile(indexFile)));
             } else if(corpusFile.getName().endsWith(".tar.gz") || corpusFile.getName().endsWith(".tgz")) {
                if(indexFile == null) {
                    badOptions(p, "Corpus file is a ZIP but index file is null");
                }
                if(outputFile == null) {
                    badOptions(p, "Corpus file is a ZIP but output file not specified");
                }
                corpus = CorpusTools.fromTarball(corpusFile, new File(outputFile.getParent(), "docs"));
                mapper.writeValue(outputFile, toIndexed(corpus, SaffronPath.fromFile(indexFile)));
                 
             } else {
                IndexedCorpus icorpus = mapper.readValue(corpusFile, IndexedCorpus.class);
                if(indexFile != null && !indexFile.equals(icorpus.index)) {
                    System.err.println("Using " + indexFile + " as index not " + icorpus.index + " as in metadata");
                    corpus = new IndexedCorpus(icorpus.documents, SaffronPath.fromFile(indexFile));
                } else {
                    corpus = icorpus;
                }
            }

            DocumentSearcherFactory.loadSearcher(corpus, indexFile, true);
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }
    
    private static IndexedCorpus toIndexed(Corpus corpus, SaffronPath indexFile) {
        if(corpus instanceof IndexedCorpus) {
            return (IndexedCorpus)corpus;
        } else {
            List<Document> docs = new ArrayList<>();
            for(Document d : corpus.getDocuments()) {
                docs.add(d);
            }
            return new IndexedCorpus(docs, indexFile);
        }
    }
}
