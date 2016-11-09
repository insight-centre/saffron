package org.insightcentre.nlp.saffron.topic.stats;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

/**
 * The main class for topic statiastic generation
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
                accepts("c", "The configuration to use (optional)").withRequiredArg().ofType(File.class);
                accepts("t", "The input topic files").withRequiredArg().ofType(File.class);
                accepts("d", "The document-topic alignments").withRequiredArg().ofType(File.class);
                accepts("ot", "Where to write the output topics to").withRequiredArg().ofType(File.class);
                accepts("od", "Where to write the output document-topics to").withRequiredArg().ofType(File.class);
            }};
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
 
            final File configuration = (File)os.valueOf("c");
            if(configuration != null && !configuration.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            final File docTopicModelFile = (File)os.valueOf("d");
            if(docTopicModelFile == null || !docTopicModelFile.exists()) {
                badOptions(p, "Corpus does not exist");
            }
            final File topicFile = (File)os.valueOf("t");
            if(topicFile == null || !topicFile.exists()) {
                badOptions(p, "Topic file does not exist");
            }
            final File outputTopicFile = (File)os.valueOf("ot");
            if(outputTopicFile == null) {
                badOptions(p, "Output not specified");
            }
            final File outputDocTopicFile = (File)os.valueOf("od");
            if(outputDocTopicFile == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory jsonFactory = new JsonFactory();

            Configuration config = configuration == null ? new Configuration () : mapper.readValue(configuration, Configuration.class);
            List<Topic> topics = mapper.readValue(topicFile, mapper.getTypeFactory().constructCollectionType(List.class, Topic.class));
            List<DocumentTopic> docTopics = mapper.readValue(docTopicModelFile, mapper.getTypeFactory().constructCollectionType(List.class, DocumentTopic.class));

            TopicStats ts = new TopicStats(config.min_score);

            ts.addTopicStats(topics, docTopics);

            ObjectWriter ow = mapper.writerWithDefaultPrettyPrinter();
            ow.writeValue(outputTopicFile, topics);
            ow.writeValue(outputDocTopicFile, docTopics);
            
            
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }
}
