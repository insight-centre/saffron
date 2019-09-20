package org.insightcentre.nlp.saffron.topic.topicsim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.TopicSimilarityConfiguration;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TopicSimilarity {

    private final double threshold;
    private final int top_n;

    public TopicSimilarity(TopicSimilarityConfiguration config) {
        this.threshold = config.threshold;
        this.top_n = config.topN;
    }

    public List<TopicTopic> topicSimilarity(List<DocumentTopic> documentTopics) {
        return topicSimilarity(documentTopics, new DefaultSaffronListener());
    }
    
    public List<TopicTopic> topicSimilarity(List<DocumentTopic> documentTopics, SaffronListener log) {
        List<TopicTopic> topicTopics = new ArrayList<>();
        Map<String, Object2IntMap<String>> vectors = new HashMap<>();
        log.log(String.format("%s doc-topics\n", documentTopics.size()));
        for (DocumentTopic dt : documentTopics) {
            if (!vectors.containsKey(dt.topic_string)) {
                vectors.put(dt.topic_string, new Object2IntOpenHashMap<String>());
            }
            //if(dt.occurrences != null)
            vectors.get(dt.topic_string).put(dt.document_id, dt.occurrences);
        }
        for (String t1 : vectors.keySet()) {
            TreeSet<TopicTopic> topN = new TreeSet<>(new Comparator<TopicTopic>() {

                @Override
                public int compare(TopicTopic arg0, TopicTopic arg1) {
                    int i1 = Double.compare(arg0.similarity, arg1.similarity);
                    if (i1 == 0) {
                        int i2 = arg0.topic1.compareTo(arg1.topic1);
                        if (i2 == 0) {
                            return arg0.topic2.compareTo(arg1.topic2);
                        }
                        return i2;
                    }
                    return i1;
                }
            });
            for (String t2 : vectors.keySet()) {
                if (!t1.equals(t2)) {
                    double s = sim(vectors.get(t1), vectors.get(t2));
                    if (s > threshold) {
                        topN.add(new TopicTopic(t1, t2, s));
                    }
                }
            }
            while (topN.size() > top_n) {
                topN.pollFirst();
            }
            topicTopics.addAll(topN);
        }

        return topicTopics;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static TopicSimilarity fromJsonString(String json) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, TopicSimilarity.class);
    }


    private double sim(Object2IntMap<String> v1, Object2IntMap<String> v2) {
        double aa = 0, bb = 0, ab = 0;
        for (String s : v1.keySet()) {
            double a = v1.get(s);
            aa += a * a;
            if (v2.containsKey(s)) {
                ab += a * v2.get(s);
            }
        }
        for (String s : v2.keySet()) {
            double b = v2.get(s);
            bb += b * b;
        }
        if (aa == 0 || bb == 0) {
            return 0;
        }
        return ab / Math.sqrt(aa * bb);
    }
}
