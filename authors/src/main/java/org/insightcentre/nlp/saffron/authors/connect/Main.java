package org.insightcentre.nlp.saffron.authors.connect;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

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
                accepts("c", "The configuration").withRequiredArg().ofType(File.class);
                accepts("t", "The text corpus").withRequiredArg().ofType(File.class);
                accepts("d", "The document topic mapping").withRequiredArg().ofType(File.class);
                accepts("p", "The topics").withRequiredArg().ofType(File.class);
                accepts("o", "The output author-topic mapping").withRequiredArg().ofType(File.class);
            }};
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
 
            final File configurationFile = (File)os.valueOf("c");
            if(configurationFile != null && !configurationFile.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            final File corpusFile = (File)os.valueOf("t");
            if(corpusFile == null || !corpusFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }
            final File docTopicFile = (File)os.valueOf("d");
            if(docTopicFile == null || !docTopicFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }
            final File topicFile = (File)os.valueOf("p");
            if(topicFile == null || !topicFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }

            final File output = (File)os.valueOf("o");
            if(output == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();

            Configuration config          = configurationFile == null ? new Configuration() : mapper.readValue(configurationFile, Configuration.class);
            IndexedCorpus corpus                 = mapper.readValue(corpusFile, IndexedCorpus.class);
            List<DocumentTopic> docTopics = mapper.readValue(docTopicFile, mapper.getTypeFactory().constructCollectionType(List.class, DocumentTopic.class));
            List<Topic> topics            = mapper.readValue(topicFile, mapper.getTypeFactory().constructCollectionType(List.class, Topic.class));

            ConnectResearchers cr = new ConnectResearchers(config.top_n);

            Collection<AuthorTopic> authorTopics = cr.connectResearchers(topics, docTopics, corpus.documents);
            
            mapper.writerWithDefaultPrettyPrinter().writeValue(output, authorTopics);
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public static class Configuration {
        public int top_n = 100;
    }

}
