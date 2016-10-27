package org.insightcentre.nlp.saffron.taxonomy.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.insightcentre.nlp.saffron.taxonomy.db.saffron2.MemoryDAO;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class SaffronDB {

    public final List<Saffron2Paper> papers; 
    public final List<Topic> topics;
    public final List<PaperTopic> paperTopics;

    @JsonCreator
    public SaffronDB(@JsonProperty(value="papers") List<Saffron2Paper> papers, 
        @JsonProperty(value="topics") List<Topic> topics, 
        @JsonProperty(value="paperTopics") List<PaperTopic> paperTopics) {
        this.papers = papers;
        this.topics = topics;
        this.paperTopics = paperTopics;
    }

    public List<Saffron2Paper> getPapers() {
        return papers;
    }

    public List<Topic> getTopics() {
        return topics;
    }

    public List<PaperTopic> getPaperTopics() {
        return paperTopics;
    }

}
