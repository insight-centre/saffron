package org.insightcentre.nlp.saffron.authors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;
import org.insightcentre.nlp.saffron.data.Document;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Consolidate {
   private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: "  + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
  
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {{
                accepts("t", "The input text corpus").withRequiredArg().ofType(File.class);
                accepts("o", "The output text corpus (with consolidated authors)").withRequiredArg().ofType(File.class);
            }};
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
 
            final File corpusFile  = (File)os.valueOf("t");
            if(corpusFile == null || !corpusFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }
            final File output = (File)os.valueOf("o");
            if(output == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();

            IndexedCorpus corpus        = mapper.readValue(corpusFile, IndexedCorpus.class);

            Set<Author> authors = extractAuthors(corpus);
            
            Map<Author, Set<Author>> consolidation = ConsolidateAuthors.consolidate(authors);

            IndexedCorpus corpus2 = applyConsolidation(corpus, consolidation);

            mapper.writerWithDefaultPrettyPrinter().writeValue(output, corpus2);
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public static Set<Author> extractAuthors(Corpus corpus) {
        Set<Author> authors = new HashSet<Author>();
        for(Document doc : corpus.getDocuments()) {
            for(Author author : doc.getAuthors()) {
                authors.add(author);
            }
        }
        return authors;
    }

    public static IndexedCorpus applyConsolidation(IndexedCorpus corpus, Map<Author, Set<Author>> consolidation) {
        Map<Author, Author> rmap = new HashMap<>();
        for(Map.Entry<Author, Set<Author>> e1 : consolidation.entrySet()) {
            for(Author a1 : e1.getValue()) {
                rmap.put(a1, e1.getKey());
            }
        }

        List<Document> documents = new ArrayList<>();
        for(Document document : corpus.getDocuments()) {
            List<Author> authors2 = new ArrayList<>();
            for(Author a : document.authors) {
                authors2.add(rmap.get(a));
            }
            Document doc2 = new Document(document.file, document.id, document.url,
                    document.name, document.mimeType, authors2, document.metadata, 
                    document.getContents());
            documents.add(doc2);
        }
        return new IndexedCorpus(documents, corpus.index);
    }
 
}
