package org.insightcentre.nlp.saffron.topic.ngrams;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Topic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class FilterNGrams {

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("t", "The list of topics to read").withRequiredArg().ofType(File.class);
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("o", "The list of topics to write to").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            final File input = (File) os.valueOf("t");
            if (input == null || !input.exists()) {
                badOptions(p, "Input does not exist");
            }
            final File configFile = (File) os.valueOf("c");
            if (configFile == null || !configFile.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            final File output = (File) os.valueOf("o");
            if (output == null) {
                badOptions(p, "Output not specified");
            }

            ObjectMapper mapper = new ObjectMapper();
            Configuration config = mapper.readValue(configFile, Configuration.class);

            List<Topic> outTopics = new ArrayList<>();
            Iterator<Topic> topics = mapper.readValues(new JsonFactory().createParser(input), Topic.class);
            while (topics.hasNext()) {
                Topic topic = topics.next();
                if (!is_invalid(topic)) {
                    outTopics.add(topic);
                }
            }

            mapper.writeValue(output, outTopics);

        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    private static boolean is_invalid(Topic topic) {
        if(topic.dbpediaURL != null)
            return false;
        String[] words = topic.topicString.split("\\s+");
        // The original Saffron implementation only filters topics of exactly
        // Length 2... We should probably fix this
        if(words.length != 2)
            return false;
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static class Configuration {

        File database = new File("models/ngrams.db");

        public File getDatabase() {
            return database;
        }

        public void setDatabase(File database) {
            this.database = database;
        }

    }

}
