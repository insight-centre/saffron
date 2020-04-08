package org.insightcentre.nlp.saffron.config;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * The configuration of a Saffron run
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Configuration {
    /** The configuration for the term extraction process */
    public TermExtractionConfiguration termExtraction = new TermExtractionConfiguration();

    /** The configuration for the author-term link extraction process */
    @JsonAlias("authorTopic") //Enable compatibility with 3.3
    public AuthorTermConfiguration authorTerm = new AuthorTermConfiguration();

    /** The configuration for the author-author similarity process */
    public AuthorSimilarityConfiguration authorSim = new AuthorSimilarityConfiguration();

    /** The configuration for the term-term similarity process */
    @JsonAlias("topicSim") //Enable compatibility with 3.3
    public TermSimilarityConfiguration termSim = new TermSimilarityConfiguration();

    public ConceptConsolidationConfiguration conceptConsolidation = new ConceptConsolidationConfiguration();

    /** The configuration for the taxonomy extraction process */
    public TaxonomyExtractionConfiguration taxonomy = new TaxonomyExtractionConfiguration();

    /** The configuration for knowledge graph extraction process **/
    public KnowledgeGraphExtractionConfiguration kg = new KnowledgeGraphExtractionConfiguration();

    @Override
    public String toString() {
        return String.format("{ %s }", termExtraction.toString(), authorTerm.toString(), authorSim.toString(), termSim.toString(), conceptConsolidation.toString(), taxonomy.toString());
    }

}
