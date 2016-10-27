package org.insightcentre.nlp.saffron.taxonomy.db.saffron2;

import org.insightcentre.nlp.saffron.taxonomy.db.Saffron2Paper;
import org.insightcentre.nlp.saffron.taxonomy.db.PaperTopic;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.insightcentre.nlp.saffron.taxonomy.db.DAO;
import org.insightcentre.nlp.saffron.taxonomy.db.SaffronDB;
import org.insightcentre.nlp.saffron.taxonomy.db.Topic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class MemoryDAO implements DAO {
    private final List<Saffron2Paper> papers;

    private final List<Topic> topics;
    private final Map<String, List<Topic>> slug2Topic;
    private final Map<String, Topic> id2Topic;

    private final Map<String, List<String>> topic2papers;

   

    public MemoryDAO(SaffronDB db) {
        this.papers = db.papers;
        this.topics = db.topics;
        this.topic2papers = new HashMap<>();
        for(PaperTopic pt : db.paperTopics) {
            if(!topic2papers.containsKey(pt.topicId)) {
                topic2papers.put(pt.topicId, new LinkedList<String>());
            }
            topic2papers.get(pt.topicId).add(pt.paperId);
        }
        this.slug2Topic = new HashMap<>();
        this.id2Topic = new HashMap<>();
        for(Topic t : topics) {
            if(!slug2Topic.containsKey(t.getRootSequence())) {
                slug2Topic.put(t.getRootSequence(), new LinkedList<Topic>());
            }
            slug2Topic.get(t.getRootSequence()).add(t);
            if(id2Topic.containsKey(t.getPreferredString())) {
                System.err.println("Duplicate topic identifiers");
            }
            id2Topic.put(t.getPreferredString(), t);
        }
        
        Collections.sort(topics);
    }
    
    @Override
    public Integer numDocuments() throws SQLException {
        return papers.size();
    }

    @Override
    public List<String> topRankingTopicStrings(int num) throws SQLException {
        List<String> topicNames = new ArrayList<>(num);
        for(Topic t : topics.subList(0, Math.min(topics.size(), num))) {
            topicNames.add(t.getPreferredString());
        }
        return topicNames;
    }

    @Override
    public Integer calculateTotalTokensNo() {
		// TODO: Since it's just a normalising thing, this should not affect the
		// results.
        return Integer.MAX_VALUE;
    }

    @Override
    public Integer selectCountJointTopics(String rootSequence, String rootSequence2) throws SQLException {
        final List<Topic> t1s = slug2Topic.get(rootSequence); 
        final List<Topic> t2s = slug2Topic.get(rootSequence2);
        if(t1s == null || t2s == null) {
            return 0;
        }
        int count = 0;
        for(Topic t1 : t1s) {
            for(Topic t2 : t2s) {
                final List<String> pt1s = topic2papers.get(t1.getPreferredString());
                final List<String> pt2s = topic2papers.get(t2.getPreferredString());
                if(pt1s == null || pt2s == null) {
                    continue;
                }
                for(String pt1 : pt1s) {
                    for(String pt2 : pt2s) {
                        if(pt1.equals(pt2)) {
                            count++;
                            break;
                        }
                    }
                }
            }
        }
        return count;
    }

    @Override
    public Iterator<Saffron2Paper> getPapers() throws SQLException {
        return papers.iterator();
    }

    @Override
    public Topic getTopic(String preferredString) throws SQLException {
        return id2Topic.get(preferredString);
    }
 
}
