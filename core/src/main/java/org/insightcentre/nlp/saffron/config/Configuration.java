package org.insightcentre.nlp.saffron.config;

/**
 * The configuration of a Saffron run
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Configuration {
    /** The configuration for the term extraction process */
    public TermExtractionConfiguration termExtraction = new TermExtractionConfiguration();
    /** The configuration for the author-topic link extraction process */
    public AuthorTopicConfiguration authorTopic = new AuthorTopicConfiguration();
    /** The configuration for the author-author similarity process */
    public AuthorSimilarityConfiguration authorSim = new AuthorSimilarityConfiguration();
    /** The configuration for the topic-topic similarity process */
    public TopicSimilarityConfiguration topicSim = new TopicSimilarityConfiguration();
    /** The configuration for the taxonomy extraction process */
    public TaxonomyExtractionConfiguration taxonomy = new TaxonomyExtractionConfiguration();
    
}
