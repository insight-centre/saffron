package org.insightcentre.nlp.saffron.config;

/**
 * The configuration of a Saffron run
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Configuration {
    /** The configuration for the term extraction process */
    public TermExtractionConfiguration termExtraction = new TermExtractionConfiguration();
    /** The configuration for the author-term link extraction process */
    public AuthorTermConfiguration authorTerm = new AuthorTermConfiguration();
    /** The configuration for the author-author similarity process */
    public AuthorSimilarityConfiguration authorSim = new AuthorSimilarityConfiguration();
    /** The configuration for the term-term similarity process */
    public TermSimilarityConfiguration termSim = new TermSimilarityConfiguration();
    /** The configuration for the taxonomy extraction process */
    public TaxonomyExtractionConfiguration taxonomy = new TaxonomyExtractionConfiguration();

    @Override
    public String toString() {
        return String.format("{ %s }", termExtraction.toString(), authorTerm.toString(), authorSim.toString(), termSim.toString(), taxonomy.toString());
    }
    
}
