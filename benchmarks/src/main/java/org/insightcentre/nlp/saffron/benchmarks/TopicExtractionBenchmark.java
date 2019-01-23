package org.insightcentre.nlp.saffron.benchmarks;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Topic;

/**
 * Compares the result of two topic extractions
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TopicExtractionBenchmark {

    public static class Scores {

        public final double[] recall;
        public final double[] precision;

        public Scores(double[] recall, double[] precision) {
            this.recall = recall;
            this.precision = precision;
        }

    }

    public static double averagePrecisionAtK(Scores s) {
        assert (s != null && s.recall != null && s.precision != null);
        assert (s.recall.length == s.precision.length);
        assert (s.recall.length > 0);
        double avp = s.precision[0] * s.recall[0];
        for (int i = 1; i < s.recall.length; i++) {
            avp += s.precision[i] * (s.recall[i] - s.recall[i - 1]);
        }
        return avp;
    }

    public static Scores evaluate(List<Topic> extracted, List<Topic> gold, boolean retain) {
        Collections.sort(extracted);
        final Set<String> extractedTopics = new TreeSet<>();
        if (retain) {
            for (Topic t : extracted) {
                extractedTopics.add(t.topicString.toLowerCase());
            }
        }
        Set<String> goldTopics = new TreeSet<>();
        for (Topic t : gold) {
            if (!retain || extractedTopics.contains(t.topicString.toLowerCase())) {
                goldTopics.add(t.topicString.toLowerCase());
            }
        }
        final int K = Math.max(extracted.size(), goldTopics.size());
        int found = 0;
        double[] recall = new double[K];
        double[] precision = new double[K];
        for (int i = 0; i < K; i++) {
            if (i < extracted.size() && goldTopics.contains(extracted.get(i).topicString.toLowerCase())) {
                found++;
            }
            recall[i] = (double) found / goldTopics.size();
            precision[i] = (double) found / Math.min(extracted.size(), i + 1);
        }
        return new Scores(recall, precision);
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
                    accepts("o", "The output topic file").withRequiredArg().ofType(File.class);
                    accepts("g", "The gold topic file").withRequiredArg().ofType(File.class);
                    accepts("r", "Do not limit the system to only the reference topics (topics also in gold standard)");
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            File outFile = (File) os.valueOf("o");
            if (outFile == null) {
                badOptions(p, "Output file not given");
            }

            File goldFile = (File) os.valueOf("g");
            if (goldFile == null) {
                badOptions(p, "Gold file not given");
            }

            List<Topic> extracted = mapper.readValue(outFile, mapper.getTypeFactory().constructCollectionType(List.class, Topic.class));
            List<Topic> gold = mapper.readValue(goldFile, mapper.getTypeFactory().constructCollectionType(List.class, Topic.class));

            Scores scores = evaluate(extracted, gold, !os.has("r"));

            System.out.println("|      K | Precision@K | Recall@K |");
            System.out.println("|--------+-------------+----------|");
            for (int i = 0; i < Math.min(9, scores.precision.length); i++) {
                System.out.printf("| % 6d |    %.4f   |  %.4f  |\n", i + 1, scores.precision[i], scores.recall[i]);
            }
            for (int i = 9; i < Math.min(99, scores.precision.length); i += 10) {
                System.out.printf("| % 6d |    %.4f   |  %.4f  |\n", i + 1, scores.precision[i], scores.recall[i]);
            }
            for (int i = 99; i < Math.min(999, scores.precision.length); i += 100) {
                System.out.printf("| % 6d |    %.4f   |  %.4f  |\n", i + 1, scores.precision[i], scores.recall[i]);
            }
            for (int i = 999; i < scores.precision.length; i += 1000) {
                System.out.printf("| % 6d |    %.4f   |  %.4f  |\n", i + 1, scores.precision[i], scores.recall[i]);
            }
            System.out.println();
            System.out.printf("Ave Precision: %.4f\n", averagePrecisionAtK(scores));
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }
}
