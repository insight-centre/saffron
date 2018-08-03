package org.insightcentre.nlp.saffron.topic.enrich;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcherFactory;
import org.insightcentre.nlp.saffron.topic.tfidf.TFIDF;

/**
 * This is used to create a Doc-Topics file from a taxonomy, such as those used
 * in TExEval
 *
 * @author John McCrae <john@mccr.ae>
 */
public class EnrichTopics {
    
    /**
     * Count how many times t occurs in s
     * @param s
     * @param t
     * @return 
     */
    private static int count(String[] s, String[] t) {
        int n = 0;
        OUTER: for(int i = 0; i < s.length; i++) {
            if(s[i].equals(t[0])) {
                for(int j = 1; j < t.length && i + j < s.length; j++) {
                    if(!s[i+j].equals(t[j]))
                        continue OUTER;
                }
                n++;
            }
        }
        return n;
    }

    public static Result enrich(Set<String> topicStrings, IndexedCorpus corpus) {
        List<DocumentTopic> docTopics = new ArrayList<>();
        List<Topic> topics = new ArrayList<>();
        Object2IntMap<String> topicFreq = new Object2IntOpenHashMap<>();
        Object2IntMap<String> docFreq = new Object2IntOpenHashMap<>();
        for (Document d : corpus.getDocuments()) {
            String[] contents = d.contents().toLowerCase().split("\\s+");
            for (String seed : topicStrings) {
                String[] topic = seed.split("\\s+");
                int n = count(contents, topic);
                if(n > 0) {
                    DocumentTopic dt = new DocumentTopic(d.id, seed, n, null, null, null);
                    docTopics.add(dt);
                    topicFreq.put(seed, topicFreq.getInt(seed) + n);
                    docFreq.put(seed, docFreq.getInt(seed) + 1);
                }
            }
        }
        for(String topic : topicStrings) {
            topics.add(new Topic(topic, topicFreq.getInt(topic), docFreq.getInt(topic), 
                    (double)docFreq.getInt(topic) / corpus.getDocuments().size(), 
                    Collections.EMPTY_LIST));
        }
        
        TFIDF.addTfidf(docTopics);

        return new Result(docTopics, topics);
    }

    public static class Result {

        public final List<DocumentTopic> docTopics;
        public final List<Topic> topics;

        public Result(List<DocumentTopic> docTopics, List<Topic> topics) {
            this.docTopics = docTopics;
            this.topics = topics;
        }

    }

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
                    accepts("t", "The taxonomy to enrich (in TExEval format)").withRequiredArg().ofType(File.class);
                    accepts("c", "The corpus to load").withRequiredArg().ofType(File.class);
                    accepts("o", "The output topic list").withRequiredArg().ofType(File.class);
                    accepts("d", "The output doc-topic list").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            File topicOutFile = (File) os.valueOf("o");
            if (topicOutFile == null) {
                badOptions(p, "Output file not given");
            }

            File docTopicOutFile = (File) os.valueOf("d");
            if (docTopicOutFile == null) {
                badOptions(p, "Output file not given");
            }

            File taxoFile = (File) os.valueOf("t");
            if (taxoFile == null || !taxoFile.exists()) {
                badOptions(p, "The taxonomy does not exist");
            }

            File corpusFile = (File) os.valueOf("c");
            if (corpusFile == null || !corpusFile.exists()) {
                badOptions(p, "The corpus file does not exist");
            }

            final IndexedCorpus corpus = mapper.readValue(corpusFile, IndexedCorpus.class);
            DocumentSearcherFactory.loadSearcher(corpus);

            final Set<String> topics = readTExEval(taxoFile);

            Result res = enrich(topics, corpus);

            mapper.writerWithDefaultPrettyPrinter().writeValue(topicOutFile, res.topics);
            mapper.writerWithDefaultPrettyPrinter().writeValue(docTopicOutFile, res.docTopics);
        } catch (Exception x) {
            x.printStackTrace();
            return;
        }
    }

    private static Set<String> readTExEval(File goldFile) throws IOException {
        HashSet<String> links = new HashSet<>();
        String line;
        try (BufferedReader reader = new BufferedReader(new FileReader(goldFile))) {
            while ((line = reader.readLine()) != null) {
                if (!line.equals("")) {
                    String[] elems = line.split("\t");
                    if (elems.length != 2) {
                        throw new IOException("Bad Line: " + line);
                    }
                    links.add(elems[0].toLowerCase());
                    links.add(elems[1].toLowerCase());
                }
            }
        }
        return links;
    }
}
