package org.insightcentre.nlp.saffron.taxonomy.search;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;

/**
 *
 * @author John McCrae
 */
public class SumScore implements TaxonomyScore {
    private final SupervisedTaxo classifier;
    private final Object2DoubleMap<TaxoLink> scores = new Object2DoubleOpenHashMap<>();

    public SumScore(SupervisedTaxo classifier) {
        this.classifier = classifier;
    }
    
    @Override
    public double score(Solution solution) {
        double score = 0.0;
        for(Taxonomy t : solution.heads.values()) {
            score += scoreTaxo(t);
        }
        return score;
    }
    
    private double scoreTaxo(Taxonomy taxo) {
        double score = 0.0;
        for(Taxonomy t2 : taxo.children) {
            final TaxoLink tl = new TaxoLink(taxo.root, t2.root);
            if(!scores.containsKey(tl))
                scores.put(tl, classifier.predict(taxo.root, t2.root));
            score += scores.getDouble(tl);
            score += scoreTaxo(t2);
        }
        return score;
    }

}
