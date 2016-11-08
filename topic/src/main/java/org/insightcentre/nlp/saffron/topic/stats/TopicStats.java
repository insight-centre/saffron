package org.insightcentre.nlp.saffron.topic.stats;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.Topic.MorphologicalVariation;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class TopicStats {
    public final double min_score; // = 1.7

    public TopicStats(double min_score) {
        this.min_score = min_score;
    }

    public void addTopicStats(List<Topic> topics, List<DocumentTopic> docTopics) {
        Map<String, List<Topic>> embeddedness_map = create_embeddedness_map(topics);
        Object2IntMap<Topic> embeddedness = calculate_embeddedness(topics, embeddedness_map);
        
        // Do this first so filtered topics are not counted in unembedded occurrences
        score_topics(topics, min_score, embeddedness);
        
        Object2IntMap<DocumentTopic> unembedded_occ = calculate_unembedded_occ(topics, embeddedness_map, docTopics);

        TFIDFScoreCalculator tfidf_score_calc = new TFIDFScoreCalculator(topics, docTopics);
        
        Map<String, Topic> topics_by_id = topicsById(topics);
        Object2IntMap<String> paper_count = countDocuments(docTopics);
        int total_papers = countDocs(docTopics);
        
        //num_pts = s.query(func.count(PaperTopic.id)).scalar()
        //q = s.query(PaperTopic).yield_per(10000)
        //for i, paper_topic in enumerate(q):
        for(DocumentTopic paper_topic : docTopics) {
            Topic t = topics_by_id.get(paper_topic.topic_string);
            tfidf_score_calc.calculate(paper_topic, paper_count.getInt(paper_topic.topic_string), total_papers, unembedded_occ);
            paper_topic.score = paper_topic.tfidf * t.score;
        }
    }

    private Map<String, List<Topic>> create_embeddedness_map(List<Topic> topics) {
        EmbeddednessCalculator emb_calc = new EmbeddednessCalculator();
        
        for(Topic topic : topics) {
            String[] tokens = topic.slug.split("_");
            emb_calc.add(tokens, topic);
        }
        return emb_calc.calculate();
    }

    private Object2IntMap<Topic> calculate_embeddedness(List<Topic> topics, Map<String, List<Topic>> embeddedness_map) {
        Object2IntMap<Topic> embeddedness = new Object2IntOpenHashMap<>();
        for(Topic topic : topics) {
            List<Topic> l = embeddedness_map.get(topic.topicString);
            if(l != null) 
                embeddedness.put(topic, l.size());
        }
        return embeddedness;
    }

    private Map<String, Topic> topicsById(List<Topic> topics) {
        Map<String, Topic> topicsById = new HashMap<>();
        for(Topic topic : topics)
            topicsById.put(topic.topicString, topic);
        return topicsById;
    }

    private Object2IntMap<String> countDocuments(List<DocumentTopic> docTopics) {
        Object2IntMap<String> docs = new Object2IntOpenHashMap<>();
        for(DocumentTopic dt : docTopics) 
            docs.put(dt.topic_string, docs.getInt(dt.topic_string) + 1);
        return docs;
    }

    private int countDocs(List<DocumentTopic> docTopics) {
        Set<String> docs = new HashSet<>();
        for(DocumentTopic dt : docTopics) 
            docs.add(dt.document_id);
        return docs.size();
    }

    private void score_topics(List<Topic> topics, double min_score, Object2IntMap<Topic> embeddedness) {
        ListIterator<Topic> topicIter = topics.listIterator();
        while(topicIter.hasNext()) {
            Topic topic = topicIter.next();
            topic.score = score_topic(topic, embeddedness.getInt(topic));
            if(topic.score < min_score) 
                topicIter.remove();
        }
    }

    private Object2IntMap<DocumentTopic> calculate_unembedded_occ(List<Topic> topics, Map<String, List<Topic>> embeddedness_map, List<DocumentTopic> docTopics) {
        EmbeddedOccurrencesCalculator unemb_calc = new EmbeddedOccurrencesCalculator(embeddedness_map);
        int num_topics = topics.size();
        Object2IntMap<Topic> unembedded_occ = new Object2IntOpenHashMap<>();
        Object2IntMap<DocumentTopic> dtm = new Object2IntOpenHashMap<>();
        for(Topic topic : topics) {
            Object2IntMap<String> embedded_occ = unemb_calc.calculate(topic, docTopics);
            for(DocumentTopic docTopic : docTopics) {
                if(docTopic.topic_string.equals(topic.topicString)) {
                    dtm.put(docTopic, embedded_occ.get(docTopic.document_id));
                }

            }

            int total_unembedded_occ = sum(embedded_occ);
            unembedded_occ.put(topic,topic.occurrences - total_unembedded_occ);
        }
        return dtm;
    }

    private double score_topic(Topic topic, int emb) {
        int words = topic.topicString.split("\\s+").length;
        int matches = topic.occurrences;
        int acronym_boost = 0;
        for(MorphologicalVariation mv : topic.mvList) {
            if(mv.acronym != null && !mv.acronym.equals(""))
                acronym_boost = 1;
        }
        return words*Math.log(1+matches) + 3.5*Math.log(1+emb) + acronym_boost;
    }

    private int sum(Object2IntMap<String> embedded_occ) {
        int sum = 0;
        for(int i : embedded_occ.values())
            sum += i;
        return sum;
    }
            


}
