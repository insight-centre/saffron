package org.insightcentre.nlp.saffron.taxonomy.metrics;

import java.util.Set;

import org.insightcentre.nlp.saffron.config.KnowledgeGraphExtractionConfiguration;
import org.insightcentre.nlp.saffron.config.TaxonomySearchConfiguration;
import org.insightcentre.nlp.saffron.taxonomy.supervised.BinaryRelationClassifier;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;

public abstract class ScoreFactory {

	/**
     * Create an instance of knowledge graph score
     * @param config The configuration
     * @param score The value of the score (should be config.score)
     * @param classifier The classification function
     * @param terms The list of terms
     * @return A taxonomy scoring object initialised for the empty taxonomy
     */
    public static Score getInstance(TaxonomySearchConfiguration config,
            TaxonomySearchConfiguration.Score score,
            BinaryRelationClassifier<String> classifier, Set<String> terms) {
        if (null != score) {
            switch (score) {
                case simple:
                    return new SumScore(classifier);
                case transitive:
                    return new TransitiveScore(classifier);
                case bhattacharryaPoisson:
                    if (config.baseScore == TaxonomySearchConfiguration.Score.bhattacharryaPoisson) {
                        throw new IllegalArgumentException("Recursive score");
                    }
                    return new BhattacharryaPoisson(ScoreFactory.getInstance(config, config.baseScore, classifier, terms),
                            terms, config.aveChildren, config.alpha);
            }
        }
        throw new IllegalArgumentException("Unknown score");
    }
    
    /**
     * Create an instance of knowledge graph score
     * @param config The configuration
     * @param score The value of the score (should be config.score)
     * @param classifier The classification function
     * @param terms The list of terms
     * @return A taxonomy scoring object initialised for the empty taxonomy
     */
    public static Score getInstance(TaxonomySearchConfiguration config, KnowledgeGraphExtractionConfiguration kgConfig,
            TaxonomySearchConfiguration.Score score,
            MulticlassRelationClassifier<String> classifier, Set<String> terms) {
        if (null != score) {
            switch (score) {
                case simple:
                    return new SumKGScore(classifier, kgConfig);
                /*case transitive:
                    return new TransitiveScore(classifier);
                case bhattacharryaPoisson:
                    if (config.baseScore == TaxonomySearchConfiguration.Score.bhattacharryaPoisson) {
                        throw new IllegalArgumentException("Recursive score");
                    }
                    return new BhattacharryaPoisson(ScoreFactory.getInstance(config, config.baseScore, classifier, terms),
                            terms, config.aveChildren, config.alpha);
                            */
            }
        }
        throw new IllegalArgumentException("Unknown score");
    }
}
