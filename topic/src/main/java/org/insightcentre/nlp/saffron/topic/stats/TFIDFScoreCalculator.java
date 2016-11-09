package org.insightcentre.nlp.saffron.topic.stats;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import static java.lang.Math.max;
import java.util.List;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
class TFIDFScoreCalculator {
    private final List<Topic> topics;
    private final Object2IntMap<String> paper_occ_map;

    public TFIDFScoreCalculator(List<Topic> topics, List<DocumentTopic> docTopics) {
        this.topics = topics;
        this.paper_occ_map = new Object2IntOpenHashMap<>();
        for(DocumentTopic dt : docTopics) {
            paper_occ_map.put(dt.document_id, paper_occ_map.getInt(dt.document_id) + 1);
        }
    }

    void calculate(DocumentTopic paper_topic, int topic_paper_count, int total_papers) {
        double topic_idf = Math.log(total_papers/(double)(topic_paper_count));
        
        int occ = max(paper_topic.unembedded_occ, 0);
        int total_occ_paper = paper_occ_map.getInt(paper_topic.document_id);
        double tf = occ/(double)(1+total_occ_paper);

        paper_topic.tfidf = tf * topic_idf;
    }

}
