package org.insightcentre.nlp.saffron.topic.stats;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
class EmbeddedOccurrencesCalculator {
    private final Map<String, List<Topic>> embeddedness_map;

    public EmbeddedOccurrencesCalculator(Map<String, List<Topic>> embeddedness_map) {
        this.embeddedness_map = embeddedness_map;
    }

    
        /**
        Embeddedness is a tree structure, e.g. "natural language" embeds everything that
        "natural language processing" embeds. Our "embeddedness map" contains all
        topics embedded by a topic (i.e. our data for each node contains all *descendants*
        in the tree). But to get the unembedded occurrences, we need to subtract only
        occurrences of the direct children, e.g. because when computing unembedded_occ
        for "natural language", the occurrences for "natural language processing" will 
        also include all occurrences of "applied natural language processing".
        
        If t1 has child t2 in the embeddedness tree, then t1 "directly embeds" t2.
        */

    Set<String> _directly_embeds(Topic topic) {
        // Get all topic ids embedded by embedded topics
        Set<String> nested_emb = new HashSet<>();
        for(Topic t : embeddedness_map.get(topic.topicString)) {    
            for(Topic t2 : embeddedness_map.get(t.topicString)) 
                nested_emb.add(t2.topicString);
        }
        // Get direct children by subtracting nested set
        Set<String> direct_emb = new HashSet<>(embeddedness_map.keySet());
        direct_emb.removeAll(nested_emb);
        return direct_emb;
    }

    Object2IntMap<String> calculate(Topic topic, List<DocumentTopic> paperTopics) {
        Set<String> direct_emb = _directly_embeds(topic);

        final Object2IntMap<String> embedded_occ;
        if(!direct_emb.isEmpty()) {
            Map<String, List<DocumentTopic>> q = new HashMap<>();
            for(DocumentTopic pt : paperTopics) {
                if(direct_emb.contains(pt.topic_string) &&
                    pt.topic_string.equals(topic.topicString)) {
                    q.get(pt.document_id).add(pt);
                }
            }
            
            // {paper_id: Sum of occurrences in paper for all directly embedded topics}
            embedded_occ = new Object2IntOpenHashMap<>();
            for(Map.Entry<String, List<DocumentTopic>> e : q.entrySet()) {
                embedded_occ.put(e.getKey(), sum(e.getValue()));
            }
        } else {
            embedded_occ = new Object2IntOpenHashMap<>();
        }
        
        return embedded_occ;
    }

    public int sum(List<DocumentTopic> dts) {
        int sum = 0;
        for(DocumentTopic dt : dts)
            sum += dt.matches;
        return sum;
    }

}
